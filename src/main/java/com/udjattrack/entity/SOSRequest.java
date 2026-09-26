package com.udjattrack.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "sos_requests")
@DiscriminatorValue("SOS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SOSRequest extends IssueRequest {

    @Column(name = "location", length = 255)
    private String location;

    @Builder.Default
    @Column(name = "is_auto_triggered")
    private Boolean autoTriggered = false;
}
