package com.keyword.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class KeywordStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
}
