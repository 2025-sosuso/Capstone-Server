package com.knu.sosuso.capstone.repository;

import com.knu.sosuso.capstone.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findBySub(String username);
}
