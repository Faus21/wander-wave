package com.dama.wanderwave.security;

import com.dama.wanderwave.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
	private final UserRepository repository;
	@Override
	public UserDetails loadUserByUsername( String userData ) throws UsernameNotFoundException {
		return repository.loadByNicknameOrEmail(userData)
				       .orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}
}
