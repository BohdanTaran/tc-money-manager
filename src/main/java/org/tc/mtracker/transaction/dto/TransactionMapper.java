package org.tc.mtracker.transaction.dto;

import org.mapstruct.*;
import org.tc.mtracker.category.CategoryMapper;
import org.tc.mtracker.category.CategoryService;
import org.tc.mtracker.transaction.Transaction;
import org.tc.mtracker.user.User;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {CategoryMapper.class, CategoryService.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TransactionMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "user", source = "user"),
            @Mapping(target = "account", ignore = true),
            @Mapping(target = "category", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "deletedAt", ignore = true),
            @Mapping(target = "receipts", ignore = true)
    })
    Transaction toEntity(TransactionCreateRequestDTO dto, User user);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "user", ignore = true),
            @Mapping(target = "account", ignore = true),
            @Mapping(target = "category", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "deletedAt", ignore = true),
            @Mapping(target = "receipts", ignore = true)
    })
    void updateEntity(TransactionCreateRequestDTO dto, @MappingTarget Transaction transaction);

    @Mapping(target = "accountId", source = "transaction.account.id")
    TransactionResponseDTO toDto(Transaction transaction, List<String> receiptsUrls);
}
