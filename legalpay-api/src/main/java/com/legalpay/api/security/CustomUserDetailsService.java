package com.legalpay.api.security;

import com.legalpay.domain.entity.Merchant;
import com.legalpay.domain.entity.Payer;
import com.legalpay.domain.repository.MerchantRepository;
import com.legalpay.domain.repository.PayerRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final MerchantRepository merchantRepository;
    private final PayerRepository payerRepository;

    public CustomUserDetailsService(MerchantRepository merchantRepository, PayerRepository payerRepository) {
        this.merchantRepository = merchantRepository;
        this.payerRepository = payerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Try to find in Merchants
        Optional<Merchant> merchant = merchantRepository.findByEmail(email);
        if (merchant.isPresent()) {
            return User.builder()
                    .username(merchant.get().getEmail())
                    .password(merchant.get().getPassword())
                    .roles("MERCHANT")
                    .build();
        }

        // Try to find in Payers
        Optional<Payer> payer = payerRepository.findByEmail(email);
        if (payer.isPresent()) {
            return User.builder()
                    .username(payer.get().getEmail())
                    .password(payer.get().getPassword())
                    .roles("PAYER")
                    .build();
        }

        throw new UsernameNotFoundException("User not found with email: " + email);
    }
}
