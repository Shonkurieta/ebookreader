package com.example.ebookreader.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ebookreader.model.Book;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> { }
