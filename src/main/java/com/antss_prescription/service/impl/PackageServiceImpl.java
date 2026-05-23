package com.antss_prescription.service.impl;

import com.antss_prescription.dto.request.CreatePackageRequest;
import com.antss_prescription.dto.response.PackageResponse;
import com.antss_prescription.entity.SubscriptionPackage;
import com.antss_prescription.exception.BusinessException;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.PackageRepository;
import com.antss_prescription.service.PackageService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class PackageServiceImpl implements PackageService {

    private final PackageRepository packageRepository;
    private final ModelMapper modelMapper;

    public PackageServiceImpl(PackageRepository packageRepository, ModelMapper modelMapper) {
        this.packageRepository = packageRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public PackageResponse createPackage(CreatePackageRequest request) {
        if (packageRepository.existsByPackageName(request.getPackageName())) {
            throw new BusinessException("Package with name '" + request.getPackageName() + "' already exists");
        }

        SubscriptionPackage pkg = new SubscriptionPackage();
        pkg.setPackageName(request.getPackageName());
        pkg.setDurationType(request.getDurationType());
        pkg.setBaseDoctorLimit(request.getBaseDoctorLimit());
        pkg.setPackagePrice(request.getPackagePrice());
        pkg.setExtraDoctorPrice(request.getExtraDoctorPrice());
        pkg.setFeatures(request.getFeatures());
        pkg.setActive(request.isActive());

        SubscriptionPackage saved = packageRepository.save(pkg);
        log.info("Package created: {}", saved.getPackageName());
        return mapToResponse(saved);
    }

    @Override
    public PackageResponse updatePackage(Long id, CreatePackageRequest request) {
        SubscriptionPackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package", id));

        pkg.setPackageName(request.getPackageName());
        pkg.setDurationType(request.getDurationType());
        pkg.setBaseDoctorLimit(request.getBaseDoctorLimit());
        pkg.setPackagePrice(request.getPackagePrice());
        pkg.setExtraDoctorPrice(request.getExtraDoctorPrice());
        pkg.setFeatures(request.getFeatures());
        pkg.setActive(request.isActive());

        SubscriptionPackage saved = packageRepository.save(pkg);
        log.info("Package updated: {}", saved.getPackageName());
        return mapToResponse(saved);
    }

    @Override
    public void deletePackage(Long id) {
        SubscriptionPackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package", id));
        pkg.setActive(false);
        packageRepository.save(pkg);
        log.info("Package soft-deleted: {}", pkg.getPackageName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageResponse> getAllPackages() {
        return packageRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PackageResponse mapToResponse(SubscriptionPackage pkg) {
        PackageResponse res = new PackageResponse();
        res.setId(pkg.getId());
        res.setPackageName(pkg.getPackageName());
        res.setDurationType(pkg.getDurationType());
        res.setBaseDoctorLimit(pkg.getBaseDoctorLimit());
        res.setPackagePrice(pkg.getPackagePrice());
        res.setExtraDoctorPrice(pkg.getExtraDoctorPrice());
        res.setFeatures(pkg.getFeatures());
        res.setActive(pkg.isActive());
        return res;
    }
}
