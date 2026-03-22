package org.tc.mtracker.transaction;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "receipt_images")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptImage {

    @Id
    @Column(name = "image_uuid", columnDefinition = "BINARY(16)", nullable = false, unique = true)
    private UUID id = UUID.randomUUID();

    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

}
