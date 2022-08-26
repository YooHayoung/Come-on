package com.comeon.meetingservice.web.meetingplace;

import com.comeon.meetingservice.domain.meetingplace.dto.MeetingPlaceModifyDto;
import com.comeon.meetingservice.domain.meetingplace.dto.MeetingPlaceRemoveDto;
import com.comeon.meetingservice.domain.meetingplace.dto.MeetingPlaceAddDto;
import com.comeon.meetingservice.domain.meetingplace.service.MeetingPlaceService;
import com.comeon.meetingservice.web.common.aop.ValidationRequired;
import com.comeon.meetingservice.web.common.response.ApiResponse;
import com.comeon.meetingservice.web.meetingplace.query.MeetingPlaceQueryService;
import com.comeon.meetingservice.web.meetingplace.request.MeetingPlaceModifyRequest;
import com.comeon.meetingservice.web.meetingplace.request.MeetingPlaceAddRequest;
import com.comeon.meetingservice.web.meetingplace.request.PlaceModifyRequestValidator;
import com.comeon.meetingservice.web.meetingplace.response.MeetingPlaceDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestController
@RequestMapping("/meeting-places")
@RequiredArgsConstructor
public class MeetingPlaceController {

    private final MeetingPlaceService meetingPlaceService;
    private final MeetingPlaceQueryService meetingPlaceQueryService;
    private final PlaceModifyRequestValidator placeModifyRequestValidator;

    @InitBinder("meetingPlaceModifyRequest")
    public void init(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(placeModifyRequestValidator);
    }

    @PostMapping
    @ValidationRequired
    @ResponseStatus(CREATED)
    public ApiResponse<Long> meetingPlaceAdd(@Validated @RequestBody MeetingPlaceAddRequest meetingPlaceAddRequest,
                                             BindingResult bindingResult) {

        MeetingPlaceAddDto meetingPlaceAddDto = meetingPlaceAddRequest.toDto();

        Long savedId = meetingPlaceService.add(meetingPlaceAddDto);

        return ApiResponse.createSuccess(savedId);
    }

    @PatchMapping("/{meetingPlaceId}")
    @ValidationRequired
    public ApiResponse meetingPlaceModify(@PathVariable("meetingPlaceId") Long meetingPlaceId,
                                          @Validated @RequestBody MeetingPlaceModifyRequest meetingPlaceModifyRequest,
                                          BindingResult bindingResult) {

        MeetingPlaceModifyDto meetingPlaceModifyDto = meetingPlaceModifyRequest.toDto();
        meetingPlaceModifyDto.setId(meetingPlaceId);

        meetingPlaceService.modify(meetingPlaceModifyDto);

        return ApiResponse.createSuccess();
    }

    @DeleteMapping("/{meetingPlaceId}")
    public ApiResponse meetingPlaceRemove(@PathVariable("meetingPlaceId") Long meetingPlaceId) {

        meetingPlaceService.remove(MeetingPlaceRemoveDto.builder().id(meetingPlaceId).build());

        return ApiResponse.createSuccess();
    }

    @GetMapping("/{meetingPlaceId}")
    public ApiResponse<MeetingPlaceDetailResponse> meetingDetail(@PathVariable("meetingPlaceId") Long meetingId) {

        return ApiResponse.createSuccess(meetingPlaceQueryService.getDetail(meetingId));
    }

}