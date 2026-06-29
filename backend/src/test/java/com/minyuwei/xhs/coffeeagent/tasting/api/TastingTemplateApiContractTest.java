package com.minyuwei.xhs.coffeeagent.tasting.api;

import com.minyuwei.xhs.coffeeagent.shared.domain.ConfirmationStatus;
import com.minyuwei.xhs.coffeeagent.shared.domain.SourceType;
import com.minyuwei.xhs.coffeeagent.support.ApiContractTestSupport;
import com.minyuwei.xhs.coffeeagent.tasting.application.TastingTemplateApplicationService;
import com.minyuwei.xhs.coffeeagent.tasting.domain.BrewRecipe;
import com.minyuwei.xhs.coffeeagent.tasting.domain.CoffeeBean;
import com.minyuwei.xhs.coffeeagent.tasting.domain.SensoryScore;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TemperatureFlavor;

import java.util.List;

public class TastingTemplateApiContractTest {
    public static void run() {
        TastingTemplateController controller = new TastingTemplateController(new TastingTemplateApplicationService());
        var response = controller.save(
                "req-template",
                "s1",
                CoffeeBean.confirmed("水洗埃塞", "本地烘焙商", "埃塞", "水洗"),
                new BrewRecipe("V60", "手磨", "中细", 15, 225, 92, "1:15", "三段注水", 150),
                List.of(new SensoryScore(SensoryScore.Dimension.ACIDITY, 8, "明亮")),
                List.of(new TemperatureFlavor(TemperatureFlavor.TemperatureStage.WARM, TemperatureFlavor.SenseType.TASTE, "甜橙", "圆润甜感", TemperatureFlavor.Polarity.POSITIVE, SourceType.MODEL_SUGGESTED, ConfirmationStatus.ACCEPTED))
        );
        ApiContractTestSupport.assertTrue(response.error() == null, "模板保存必须返回成功 envelope");
        ApiContractTestSupport.assertTrue(response.data().scores().getFirst().value() == 8, "必须保存 0-10 感官评分");
        ApiContractTestSupport.assertTrue(response.data().acceptedFlavors().size() == 1, "只保存用户接受或编辑后的风味");
    }
}
