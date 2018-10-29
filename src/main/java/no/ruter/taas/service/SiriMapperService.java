package no.ruter.taas.service;

import com.hazelcast.core.IMap;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import no.ruter.taas.config.AppConfig;
import no.ruter.taas.siri.mapper.MapperService;
import no.ruter.taas.siri20.util.SiriDataType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SiriMapperService extends MapperService {

  @Autowired
  public SiriMapperService(@Autowired AppConfig config,
      @Qualifier("getQuayMap") IMap<String, String> quayMap,
      @Qualifier("getStopPlaceMap") IMap<String, String> stopPlaceMap,
      @Qualifier("getUnmappedIds") IMap<String, Map<SiriDataType, Set<String>>> unmappedIds) {
    super(config.getNetexStopplaceUrl(), config.getFallbackPath(), config.isFetchFromUrl(),
        quayMap, stopPlaceMap);
  }

}
