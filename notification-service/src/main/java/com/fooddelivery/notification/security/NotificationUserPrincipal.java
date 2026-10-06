package com.fooddelivery.notification.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class NotificationUserPrincipal implements UserDetails {

    private final Long customerId;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;

    public NotificationUserPrincipal(Long customerId, String email, Collection<? extends GrantedAuthority> authorities) {
        this.customerId = customerId;
        this.email = email;
        this.authorities = authorities;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
