package com.meteor.sample.storage;

import com.meteor.CoreDbContextTest;
import com.meteor.sample.domain.Sample;
import com.meteor.sample.domain.SampleRepository;
import com.meteor.shared.SampleStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SampleRepositoryAdapterIT extends CoreDbContextTest {

    private final SampleRepository sampleRepository;

    SampleRepositoryAdapterIT(SampleRepository sampleRepository) {
        this.sampleRepository = sampleRepository;
    }

    @Test
    void saveNewAndFind() {
        Sample saved = sampleRepository.save(Sample.create("meteor"));

        Sample found = sampleRepository.findById(saved.getId()).orElseThrow();

        assertThat(saved.isNew()).isFalse();
        assertThat(found.getName()).isEqualTo("meteor");
        assertThat(found.getStatus()).isEqualTo(SampleStatus.ACTIVE);
        assertThat(found.getCreatedAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    void saveExistingAppliesDomainState() {
        Sample saved = sampleRepository.save(Sample.create("meteor"));
        saved.deactivate();

        sampleRepository.save(saved);

        Sample found = sampleRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(SampleStatus.INACTIVE);
    }

}
