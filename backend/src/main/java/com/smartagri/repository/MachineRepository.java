package com.smartagri.repository;

import com.smartagri.entity.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MachineRepository extends JpaRepository<Machine, Long> {
    Optional<Machine> findByMachineId(String machineId);
}
