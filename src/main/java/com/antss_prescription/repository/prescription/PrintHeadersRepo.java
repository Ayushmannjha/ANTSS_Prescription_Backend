package com.antss_prescription.repository.prescription;

import com.antss_prescription.entity.prescription.PrintHeaders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrintHeadersRepo extends JpaRepository<PrintHeaders, Long> {

    Optional<PrintHeaders> findByEntityIdAndEntityTypeIgnoreCase(long entityId, String entityType);

    List<PrintHeaders> findByEntityTypeIgnoreCase(String entityType);

    List<PrintHeaders> findByEntityId(long entityId);

    List<PrintHeaders> findByEntityIdAndEntityTypeIgnoreCaseOrderByUpdatedAtDesc(long entityId, String entityType);

    List<PrintHeaders> findAllByOrderByUpdatedAtDesc();
}
