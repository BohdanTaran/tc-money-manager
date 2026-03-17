package org.tc.mtracker.transaction;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private TransactionType type;

    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;

    private BigDecimal amount;

    private String description;

    private List<UUID> imageIds;

    private LocalDate date;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

}
