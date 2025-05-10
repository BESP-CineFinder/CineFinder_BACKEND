package com.cinefinder.user.data.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinefinder.user.data.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {


	Optional<User> findByGoogleSub(String googleSub);
}
