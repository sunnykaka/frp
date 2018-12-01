package com.tuandai.learn.frp.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndexRankResult {

    private List<ADIndex> adIndices;

}
