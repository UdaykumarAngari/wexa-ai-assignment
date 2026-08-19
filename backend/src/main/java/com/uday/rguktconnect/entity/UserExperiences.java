package com.uday.rguktconnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserExperiences {
    private Long id;
    @JsonIgnore
    private User user;
    private String title;
    private String companyName;
    private String location;
    private String employmentType;
    private String locationType;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isCurrentRole;
    private String description;

    @JsonProperty("formattedEndDate")
    public String getFormattedEndDate() {
        if (this.isCurrentRole || this.endDate == null) {
            return "Present";
        }
        return this.endDate.toString();
    }
}