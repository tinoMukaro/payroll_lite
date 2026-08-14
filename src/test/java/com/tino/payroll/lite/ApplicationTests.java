package com.tino.payroll.lite;

import com.tino.payroll.lite.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class ApplicationTests {

	@Autowired
	private AuditService auditService;

	@Test
	void contextLoads() {
	}

	@Test
	void auditSearchAcceptsOmittedFiltersAgainstPostgreSql() {
		assertDoesNotThrow(() ->
				auditService.search(null, null, null, null, null, 0, 5)
		);
	}

}
