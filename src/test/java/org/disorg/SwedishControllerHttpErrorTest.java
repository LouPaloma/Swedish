package org.disorg;

import static org.junit.Assert.*;

import java.io.File;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
	    properties = {
	    		"throwError:true"
	    		, "errorThreshold:1"
	    }
)
/**
 * Test case for HTTP error conditions
 * 
 * @author billsa
 *
 */
public class SwedishControllerHttpErrorTest {

    @Autowired
    private ApplicationContext ctx;

	@Test
	public void testFile1() throws Exception {
		File file = new File(this.getClass().getResource("../../File1.txt").toURI());
		System.err.println("File: " + file.getAbsolutePath());
		
		CommandLineRunner runner = ctx.getBean(CommandLineRunner.class);
		runner.run(file.getAbsolutePath());
		assertEquals("invalid return code", SwedishErrorCodes.SERVER_RESPONSE_THRESHOLD, ((ExitCodeGenerator) runner).getExitCode());
		
	}
}
