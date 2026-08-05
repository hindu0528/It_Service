package com.ticketdesk.ticketservice.repository;

import com.ticketdesk.ticketservice.entity.Ticket;
import com.ticketdesk.ticketservice.enums.TicketCategory;
import com.ticketdesk.ticketservice.enums.TicketPriority;
import com.ticketdesk.ticketservice.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    long countByStatus(TicketStatus status);

    long countByPriority(TicketPriority priority);

    long countByCreatedBy(Long createdBy);

    Page<Ticket> findByCreatedBy(Long createdBy, Pageable pageable);
}
