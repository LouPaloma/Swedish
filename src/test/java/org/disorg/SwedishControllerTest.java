package org.disorg;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class SwedishControllerTest {

    @Autowired
    private ApplicationContext ctx;

	@Test
	public void testRegex() {
		//[[["{att översätta}","{toTranslate}",null,null,1]],null,"en"]
		//[[["{ToTranslate [med parentes]}","{toTranslate[with brackets]}",null,null,3]],null,"en"]
		//[[["{ToTranslate \"med citat\"}","{toTranslate\"with quotes\"}",null,null,3]],null,"en"]
		//[[["Ord här","words here",null,null,3]],null,"en"]
		//[[["\"Citerade\" ord här","\"quoted\" words here",null,null,3]],null,"en"]

		String s = "[[[\"{att översätta}\",\"{toTranslate}\",null,null,1]],null,\"en\"]";
		String regex = "(?<!\\\\)\"";
		//System.out.println(Arrays.toString(s.split(regex)));
		Arrays.asList(s.split(regex)).forEach(System.out::println);
		System.err.println(s.split(regex)[1]);
		System.err.println(s.split(regex)[1].replace("\\\"", "\""));
		
		s = "[[[\"\\\"Citerade\\\" ord här\",\"\\\"quoted\\\" words here\",null,null,3]],null,\"en\"]";
		Arrays.asList(s.split(regex)).forEach(System.out::println);
		System.err.println(s.split(regex)[1]);
		System.err.println(s.split(regex)[1].replace("\\\"", "\""));
	}
	
	@Test
	public void testNoArgs() throws Exception {
		CommandLineRunner runner = ctx.getBean(CommandLineRunner.class);
		runner.run();
		assertEquals("invalid return code", SwedishErrorCodes.NO_FILES, ((ExitCodeGenerator) runner).getExitCode());
	}

	@Test
	public void testUnknownFile() throws Exception {
		CommandLineRunner runner = ctx.getBean(CommandLineRunner.class);
		runner.run("file1");
		assertEquals("invalid return code", SwedishErrorCodes.UNKNOWN_FILES, ((ExitCodeGenerator) runner).getExitCode());
	}

	@Test
	public void testOneBadFile() throws Exception {
		File goodFile = new File(this.getClass().getResource("../../File1.txt").toURI());

		CommandLineRunner runner = ctx.getBean(CommandLineRunner.class);
		runner.run(goodFile.getAbsolutePath(), "file1");
		assertEquals("invalid return code", SwedishErrorCodes.UNKNOWN_FILES, ((ExitCodeGenerator) runner).getExitCode());
	}

	@Test
	public void testFile1() throws Exception {
		File file = new File(this.getClass().getResource("../../File1.txt").toURI());
		System.err.println("File: " + file.getAbsolutePath());
		
		CommandLineRunner runner = ctx.getBean(CommandLineRunner.class);
		runner.run(file.getAbsolutePath());
		assertEquals("invalid return code", 0, ((ExitCodeGenerator) runner).getExitCode());
		
		//test output file contents
		File output = new File("Batched.txt");
		assertTrue("missing batched file", output.exists());
		
		//File control = new File(this.getClass().getResource("../../Batched_File1.txt").toURI());
		//assertEquals("batched file contents mismatch", new String(Files.readAllBytes(control.toPath()), StandardCharsets.UTF_8), 
				//new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8) );

		output = new File("AsYouGo.txt");
		assertTrue("missing as you go file", output.exists());
		//assertEquals("as you go file contents mismatch", new String(Files.readAllBytes(control.toPath()), StandardCharsets.UTF_8), 
				//new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8) );
	}
	
	@Test
	public void testFiles() throws Exception {
		String file1 = new File(this.getClass().getResource("../../File1.txt").toURI()).getAbsolutePath();
		String file2 = new File(this.getClass().getResource("../../File2.txt").toURI()).getAbsolutePath();
		String file3 = new File(this.getClass().getResource("../../File3.txt").toURI()).getAbsolutePath();
		
		CommandLineRunner runner = ctx.getBean(CommandLineRunner.class);
		runner.run(file1, file2, file3);
		assertEquals("invalid return code", 0, ((ExitCodeGenerator) runner).getExitCode());
		
		//test output file contents
		File output = new File("Batched.txt");
		assertTrue("missing batched file", output.exists());
		
		//File control = new File(this.getClass().getResource("../../Batched_3Files.txt").toURI());
		//assertEquals("batched file contents mismatch", new String(Files.readAllBytes(control.toPath()), "UTF-8"), new String(Files.readAllBytes(output.toPath()), "UTF-8") );

		output = new File("AsYouGo.txt");
		assertTrue("missing as you go file", output.exists());
	}
	
}
