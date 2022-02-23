package com.kohang.fsi251notifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@ComponentScan("com.kohang")
@EnableMongoRepositories
public class Application {
	
	public static void main(String[] args) {
		ApplicationContext ctx = SpringApplication.run(Application.class, args);	
		
		//FSI251Recognizer recognizer = ctx.getBean(FSI251Recognizer.class);
		
		//recognizer.run();
		
		/*FSI251Repository repository = ctx.getBean(FSI251Repository.class);
		
		final String SAMPLE_CERT_NO = "A 7956010";
		
		FSI251Data data = new FSI251Data();
		data.setCertNo(SAMPLE_CERT_NO);
		data.setFileName("testing");
		data.setCertDate("9/12/2021");
		
		repository.save(data);*/
		
		//FileAccesser fa = ctx.getBean(FileAccesser.class);
		
		//fa.copyAndDeleteFiles(fa.getSrcFiles());
		
		/*File document = new File("\\\\TravisNAS\\KoHangNAS\\KoHang\\11 FSI 251\\高衡 FS251 2021\\A7426356 大眾教室(屯門栢麗廣場) 1樓107室.pdf");
		
		List<FSI251Data> resultList = recognizer.run(document);*/
		
	}

}
