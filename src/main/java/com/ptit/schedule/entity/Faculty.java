package com.ptit.schedule.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "faculties")
@Setter
@Getter
public class Faculty {
    @Id
    private String id;

    @Column(name = "faculty_name")
    private String facultyName;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "faculty", cascade = CascadeType.ALL)
    private List<Major> majors;
}
