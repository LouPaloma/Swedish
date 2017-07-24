package org.disorg;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.disorg.service.Translator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.HttpClientErrorException;

@SpringBootApplication
@ComponentScan(basePackages = {"org.disorg"})
/**
 * Main application component.  All processing is controlled through here.
 * 
 * @author billsa
 *
 */
public class SwedishController implements CommandLineRunner, ExitCodeGenerator {
	
	/**
	 * Thread synchronization timeout, defaulted to 30 seconds
	 */
	@Value("${timeoutInSeconds:30}")
    private int timeout;
    
	/**
	 * Threshold for the number of consecutive Google API errors before terminating
	 */
	@Value("${errorThreshold:3}")
	private int errorThreshold;
	
	/**
	 * Current error count
	 */
	private AtomicInteger errorCount = new AtomicInteger();
	
	@Autowired
	private Translator translationService;
	
	private int exitCode = SwedishErrorCodes.GENERAL_ERROR;

	private static final Logger LOGGER = LoggerFactory.getLogger(SwedishController.class);
	
	/* (non-Javadoc)
	 * @see org.springframework.boot.ExitCodeGenerator#getExitCode()
	 */
	@Override
	public int getExitCode() {
		//convert to positive value if needed as required by OS
		return Math.abs(exitCode);
	}

	@Override
	final public void run(String... args) {
		//check args
		if (args.length < 1) {
			LOGGER.error("Must supply one or more files to translate.");
			exitCode = SwedishErrorCodes.NO_FILES;
			return;
		}
		
		try {
			exitCode = 0;

			//validate files - fail fast if any do not exist
			List<BufferedReader> sources = null;
			try {
				sources = getSourceFiles(args);
			} catch (IOException e) {
				exitCode = SwedishErrorCodes.UNKNOWN_FILES;
			}
			
			if (sources != null) {
				CountDownLatch latch;
				int readCounter = sources.size();
				
				//create thread pool for concurrent processing and task tracking
				Executor executor = Executors.newFixedThreadPool(readCounter);
				ArrayList<FutureTask<String>> activeTasks = new ArrayList<>();
				
				//set up output files
				BufferedWriter asYouGo = null;
				try {
					//initialize batch output
					File file = new File("Batched.txt");
					if (file.exists()) {
						file.delete();
					}

					file = new File("AsYouGo.txt");
					if (file.exists()) {
						file.delete();
					}
					asYouGo = new BufferedWriter(new FileWriter(file, true));
					
				} catch (IOException ioe) {
					LOGGER.error("FATAL error initializing output files.", ioe);
					exitCode = SwedishErrorCodes.OUTPUT_FILE_ERROR;
					return;
				}
				
				//while there are still files to read and are under the error threshold
				while ( (readCounter > 0) && (errorCount.get() < errorThreshold) ) {
					readCounter = 0;
					activeTasks.clear();
					latch = new CountDownLatch(sources.size());
					
					//read next line from all files
					for (BufferedReader source : sources) {
						String line = null;
						try {
							line = source.readLine();
						} catch (IOException e) {
							LOGGER.error("Error reading file." , e);
							exitCode = SwedishErrorCodes.FILE_READ_ERROR;
							return;
						}
						
						if (line != null) {
							readCounter++;

							//translate concurrently
							FutureTask<String> task = new FutureTask<>(new TranslationTask(line, translationService, asYouGo, latch));
							activeTasks.add(task);
							executor.execute(task);
							
							//TODO catch unchecked exceptions?
						} else {
							//decrement latch
							latch.countDown();
						}
					}
					
					//await countdownlatch for all completion
					//Java8 CompletableFuture.allOf() would allow for fast fail, but not get individual values 
			        boolean done = false;
			        try {
			            done = latch.await(timeout, TimeUnit.SECONDS );
			            if (!done) {
			                //TODO some of the jobs not done or error not caught -- catch?
			            } else {
			            	//extract results
			            	ArrayList<String> translations = new ArrayList<>();
			            	activeTasks.forEach(result -> {
			            		try {
									translations.add(result.get());
								
									//reset error count
									errorCount.set(0);
								} catch (InterruptedException | ExecutionException e) {
									//log and increment error count
									LOGGER.warn("Exception (" + errorCount.incrementAndGet() + ") during translation.", e);
									
									if (e.getCause() instanceof HttpClientErrorException) {
										HttpClientErrorException cause = (HttpClientErrorException) e.getCause();
										LOGGER.error("Request exception: " + cause.getResponseBodyAsString());
									}
								}
			            	});
			            	
			            	//sort the collection
			            	Collections.sort(translations);
			            	
			            	//write to sorted output
			            	System.err.println("Batch results: " + translations.toString());
			            	try (BufferedWriter batchOutput = new BufferedWriter(new FileWriter("Batched.txt", true))) {
			            		StringBuilder lineOut = new StringBuilder();
			            		translations.forEach(line -> {
			            			lineOut.append(line).append("\n");
			            		});
								batchOutput.write(lineOut.toString());
			            		batchOutput.flush();
							} catch (IOException e) {
								LOGGER.error("FATAL error writing batched output.", e);
								exitCode = SwedishErrorCodes.BATCH_OUTPUT_ERROR;
								return;
							}
			            }
			        } catch (InterruptedException ie) {
			        	//non-fatal
						LOGGER.warn("Processing interrupted.", ie);
			        }					
				}
				
				if (errorCount.get() >= errorThreshold) {
					LOGGER.error("FATAL: Server response threshold exceeded: " + errorCount.get());
					exitCode = SwedishErrorCodes.SERVER_RESPONSE_THRESHOLD;
				}

				//close output
				if (asYouGo != null) {
					asYouGo.close();
				}
			}
		} catch (Throwable t) {
			LOGGER.error("Unexpected error caught.", t);
			if (exitCode == 0) {
				exitCode = SwedishErrorCodes.GENERAL_ERROR;
			}
		}
	}

	/**
	 * Build a list of source file readers.  Any single exception will force an abort.
	 *  
	 * @param args string array of file FQN
	 * @return list of BufferedReaders for the files specified
	 * @throws IOException on encountering an error.  
	 */
	private List<BufferedReader> getSourceFiles(String[] args) throws IOException {
		ArrayList<BufferedReader> sources = new ArrayList<>();
		
		StringJoiner filesInError = new StringJoiner(",");
		File source;
		for (String file : args) {
			//iterate through all files, so that there would be a complete list of all the issues
			
			source = new File(file);
			//System.err.println(source.getAbsolutePath());
			
			if ( (!source.exists()) || (!source.isFile()) || (!source.canRead()) ) {
				//bad file, add to the error list
				filesInError.add(file);
			} else {
				try {
					BufferedReader bis = new BufferedReader(new FileReader(source));
					sources.add(bis);
				} catch (FileNotFoundException e) {
					LOGGER.error("Error opening file: " + file, e);
					filesInError.add(file);
				} 
			}
		}

		if (filesInError.length() == 0) {
			return sources;
		} else {
			//FATAL: close any open readers
			sources.forEach(sourceFile -> {
				try {
					sourceFile.close();
				} catch (IOException e) {
					//ignore
				}
			});
			
			String msg = "FATAL error trying to process input file(s): " + filesInError.toString();
			LOGGER.error(msg);
			throw new IOException(msg);
		}
	}

	public static void main(String[] args) {
		//run, return exit code to OS
		System.exit(SpringApplication.exit(SpringApplication.run(SwedishController.class, args)));
	}
	
}

