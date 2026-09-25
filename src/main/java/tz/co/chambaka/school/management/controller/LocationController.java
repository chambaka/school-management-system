package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.security.Access;
import tz.co.chambaka.school.management.service.LocationProxyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/location")
@PreAuthorize(Access.PEOPLE_MANAGE)
public class LocationController {

    private final LocationProxyService locationProxyService;

    public LocationController(LocationProxyService locationProxyService) {
        this.locationProxyService = locationProxyService;
    }

    @GetMapping("/regions")
    public ResponseEntity<byte[]> regions() {
        return locationProxyService.forward("/api/location/regions");
    }

    @GetMapping("/districts")
    public ResponseEntity<byte[]> districts(@RequestParam("regionId") Long regionId) {
        return locationProxyService.forward("/api/location/districts?regionId=" + regionId);
    }

    @GetMapping("/wards")
    public ResponseEntity<byte[]> wards(@RequestParam("districtId") Long districtId) {
        return locationProxyService.forward("/api/location/wards?districtId=" + districtId);
    }
}
