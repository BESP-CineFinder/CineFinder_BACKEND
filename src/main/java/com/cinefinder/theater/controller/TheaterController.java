package com.cinefinder.theater.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cinefinder.global.mapper.ResponseMapper;
import com.cinefinder.global.response.BaseResponse;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.theater.service.TheaterService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/theater")
public class TheaterController {

	private final TheaterService theaterService;

	@GetMapping("/test")
	public ResponseEntity<BaseResponse<Map<String,List<String>>>> test(@RequestParam Double lat, @RequestParam Double lon, @RequestParam(defaultValue = "1") Double distance) {
		return ResponseMapper.successOf(ApiStatus._OK, theaterService.getTheaterInfos(lat, lon, distance), TheaterController.class);
	}
}
