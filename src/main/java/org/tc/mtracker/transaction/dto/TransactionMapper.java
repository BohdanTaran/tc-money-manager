package org.tc.mtracker.transaction.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.tc.mtracker.category.CategoryMapper;
import org.tc.mtracker.category.CategoryService;
import org.tc.mtracker.transaction.Transaction;
import org.tc.mtracker.user.User;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {CategoryMapper.class, CategoryService.class})
public interface TransactionMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "category", source = "dto.categoryId", qualifiedByName = "categoryById")
    @Mapping(target = "receipts", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Transaction toEntity(TransactionCreateRequestDTO dto, User user);

    TransactionResponseDTO toDto(Transaction transaction);
}
