package com.cst438.dto;

public record OpenSectionDTO(
        int secNo,
        int year,
        String semester,
        String courseId,
        String title,
        int secId,
        String building,
        String room,
        String times,
        String instructorName,
        String instructorEmail,
        int capacity,
        long enrolledSeats,
        long availableSeats
) {
}