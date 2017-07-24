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
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
/**
 * Test class to run against actual remote google translation service
 * @author billsa
 *
 */
public class SwedishControllerRemoteTest {

    @Autowired
    private ApplicationContext ctx;

	@Test
	public void testFiles() throws Exception {
		String file1 = new File(this.getClass().getResource("../../Set1.txt").toURI()).getAbsolutePath();
		String file2 = new File(this.getClass().getResource("../../Set2.txt").toURI()).getAbsolutePath();
		String file3 = new File(this.getClass().getResource("../../Set3.txt").toURI()).getAbsolutePath();
		
		CommandLineRunner runner = ctx.getBean(CommandLineRunner.class);
		runner.run(file1, file2, file3);
		assertEquals("invalid return code", 0, ((ExitCodeGenerator) runner).getExitCode());
	}
	
}
