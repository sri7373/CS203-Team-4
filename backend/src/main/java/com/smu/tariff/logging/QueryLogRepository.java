package com.smu.tariff.logging;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QueryLogRepository extends JpaRepository<QueryLog, Long> {
    // Fetch logs with their associated user to avoid LazyInitializationException when accessed outside a transaction
    @Query("select q from QueryLog q left join fetch q.user order by q.createdAt desc")
    List<QueryLog> findAllWithUser();

    @Query("select q from QueryLog q left join fetch q.user where q.user.id = :userId and q.type = 'CALCULATE' order by q.createdAt desc")
    List<QueryLog> findByUserIdWithUser(@Param("userId") Long userId);

    @Query(value = "select q.id, q.created_at, q.user_id from query_log q where q.user_id = :userId and q.type = 'CALCULATE' order by q.created_at desc limit 50", nativeQuery = true)
    List<Object[]> findLatestRawByUser(@Param("userId") Long userId);

    @Query("select count(q) from QueryLog q where q.user.id = :userId and q.type = 'CALCULATE'")
    long countByUser_Id(@Param("userId") Long userId);
}
