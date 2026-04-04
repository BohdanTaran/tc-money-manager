package org.tc.mtracker.account.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.tc.mtracker.account.AccountService;
import org.tc.mtracker.account.dto.AccountResponseDTO;

@RestController
@RequiredArgsConstructor
public class AccountController implements AccountApi {

    private final AccountService accountService;

    @Override
    public ResponseEntity<AccountResponseDTO> getDefaultAccount(Authentication auth) {
        return ResponseEntity.ok(accountService.getDefaultAccount(auth));
    }
}
