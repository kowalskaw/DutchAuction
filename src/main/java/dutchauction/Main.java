package dutchauction;

import org.apache.log4j.PropertyConfigurator;

public class Main {

	public static void main(String[] args) throws InterruptedException {

		PropertyConfigurator.configure(System.getProperty("user.dir")+"/src/main/resources/log4j.properties");

		PerformanceCheck pc = new PerformanceCheck();
		pc.run();

		System.exit(0);
	}
}
