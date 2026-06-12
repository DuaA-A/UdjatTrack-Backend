package com.udjattrack.dto.response;
import lombok.Builder;
import lombok.Getter;
import java.util.UUID;
@Getter
@Builder
public class SuperManagerSignupResponse {
    private UUID superManagerId;
    private String email;
    private String name;
    private String status;
}
