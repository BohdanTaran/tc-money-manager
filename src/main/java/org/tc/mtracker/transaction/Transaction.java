package org.tc.mtracker.transaction;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "transactions")
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private TransactionType type;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;

    private BigDecimal amount;

    private String description;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL)
    private List<ReceiptImage> receipts;

    private LocalDate date;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

}
