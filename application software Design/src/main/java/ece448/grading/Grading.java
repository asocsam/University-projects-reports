package ece448.grading;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Grading {
	static void run(Object obj, int n) {
		ExecutorService exe = Executors.newSingleThreadExecutor();
		String className = obj.getClass().getSimpleName();		
		int grade = 0;
		try
		{
			for (int i = 0; i < n; i++)
			{
				String testCaseName = String.format("testCase%02d", i);
				try
				{
					Method testCase = obj.getClass().getDeclaredMethod(testCaseName);

					Future<Boolean> f = exe.submit(() -> {
						return (Boolean)testCase.invoke(obj);
					});

					if (f.get(60, TimeUnit.SECONDS))
					{
						System.out.printf("******** %s of %s: pass%n", testCaseName, className);
						logger.info("{} of {}: success", testCaseName, className);
						System.out.println("*****************************");
						++grade;
					}
					else
					{
						System.out.printf("******** %s of %s: fail%n", testCaseName, className);
						logger.info("{} of {}: failed", testCaseName, className);
						System.out.println("*************************************************************");
					}
				}
				catch (ExecutionException e)
				{
					System.out.printf("******** %s of %s: exception occured %s%n", testCaseName, className, e.getCause().getCause().toString());
					logger.info("{} of {}: exception {}", testCaseName, className, e.getCause().getCause().toString());
					logger.debug("{} of {}: exception", testCaseName, className, e);
					System.out.println("*************************************************************");
				}
				catch (TimeoutException e)
				{
					logger.info("{} of {}: timeout, abort", testCaseName, className);
					throw new RuntimeException(e);
				}
				catch (Throwable t)
				{
					logger.info("{} of {}: unknown error, abort", testCaseName, className, t);
					throw new RuntimeException(t);
				}
			}
		}
		finally
		{	System.out.println("");
			System.out.printf("Local Grading Test Result: %d/%d cases passed\n", grade, n);
			System.out.println("********************************************************************************");
			System.out.println("* Make sure you \"git add .\" and \"git commit -am \"your comments\" and \"git push\" *");
			System.out.println("* to fully upload all of your codes to the Endeavour Git Repo.                 *");
			System.out.println("* Otherwise, your grading result may differ from your local result vs. server  *");
			System.out.println("********************************************************************************");
			exe.shutdownNow();
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(Grading.class);
}
