package com.hogaria.service;
import com.hogaria.dto.PlanningDtos.*;import com.hogaria.entity.*;import com.hogaria.exception.*;import com.hogaria.repository.*;import org.springframework.stereotype.Service;import java.math.*;import java.util.*;
@Service public class InflationService { private final InflationIndexRepository repo; public InflationService(InflationIndexRepository repo){this.repo=repo;}
public List<InflationIndexResponse> list(Integer y){return repo.findByYearOrderByMonthAsc(y).stream().map(i->new InflationIndexResponse(i.getId(),i.getYear(),i.getMonth(),i.getRate(),i.getSource(),i.getObserved())).toList();}
public InflationIndexResponse create(InflationIndexCreateRequest r){var i=repo.save(InflationIndex.builder().year(r.year()).month(r.month()).rate(r.rate()).source(r.source()).observed(Boolean.TRUE.equals(r.observed())).notes(r.notes()).build()); return new InflationIndexResponse(i.getId(),i.getYear(),i.getMonth(),i.getRate(),i.getSource(),i.getObserved());}
public InflationAccumulatedResponse acc(int fy,int fm,int ty,int tm){var rates=repo.findByYearOrderByMonthAsc(fy).stream().filter(i->i.getMonth()>=fm&&i.getMonth()<=tm).map(InflationIndex::getRate).toList(); BigDecimal p=BigDecimal.ONE; for(var r:rates)p=p.multiply(BigDecimal.ONE.add(r)); return new InflationAccumulatedResponse(p.subtract(BigDecimal.ONE));}
}
