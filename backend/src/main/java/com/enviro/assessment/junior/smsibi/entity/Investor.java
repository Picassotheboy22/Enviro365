package com.enviro.assessment.junior.smsibi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.Period;

/**
 * An investor who owns one or more investment {@link Product}s.
 *
 * <p>Age is deliberately NOT stored as a column: it is derived from the date of birth whenever it is needed,
 * so it can never go stale (a stored "age" of 64 would silently be wrong after the investor's birthday).
 */
@Entity
@Table(name = "investors")
public class Investor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    /** Required by JPA; not for application use. */
    protected Investor() {}

    public Investor(String firstName, String lastName, String email, String phone, LocalDate dateOfBirth) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
    }

    /**
     * Age in completed years on the given date. The date is passed in (rather than using "today" internally)
     * so callers control the clock - this is what makes the age rule unit-testable.
     */
    public int ageOn(LocalDate date) {
        return Period.between(dateOfBirth, date).getYears();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
}
