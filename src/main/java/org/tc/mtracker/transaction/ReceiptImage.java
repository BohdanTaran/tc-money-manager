package org.tc.mtracker.transaction;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "receipt_images")
@Getter
@NoArgsConstructor
public class ReceiptImage {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

}
