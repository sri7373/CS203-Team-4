package com.smu.tariff.logging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface QueryLogRepository extends JpaRepository<QueryLog, Long> {
	// Fetch logs with their associated user to avoid LazyInitializationException when accessed outside a transaction
	@Query("select q from QueryLog q left join fetch q.user order by q.createdAt desc")
	List<QueryLog> findAllWithUser();
    
	// Native query to quickly inspect raw DB values including user_id (used for safe debug/verification)
	@Query(value = "select q.id, q.created_at, q.user_id from query_log q order by q.created_at desc limit 50", nativeQuery = true)
	List<Object[]> findLatestRaw();
}
