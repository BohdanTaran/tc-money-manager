package org.tc.mtracker.transaction.recurring.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.tc.mtracker.category.CategoryMapper;
import org.tc.mtracker.transaction.recurring.RecurringTransaction;
import org.tc.mtracker.user.User;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = CategoryMapper.class,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface RecurringTransactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "startDate", source = "dto.date")
    @Mapping(target = "nextExecutionDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    RecurringTransaction toEntity(RecurringTransactionCreateRequestDTO dto, User user);

    @Mapping(target = "accountId", source = "recurringTransaction.account.id")
    RecurringTransactionResponseDTO toDto(RecurringTransaction recurringTransaction);

    List<RecurringTransactionResponseDTO> toDtos(List<RecurringTransaction> recurringTransactions);
}
