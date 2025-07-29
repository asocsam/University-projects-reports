package ece448.iot_hub;

import ece448.grading.GradeP3.MqttController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PlugsResource {

  private final PlugsModel plugsModel;

  @Autowired
  public PlugsResource(PlugsModel plugsModel) {
    this.plugsModel = plugsModel;
  }

  @GetMapping("/api/plugs/{name:.+}")
  public Plug getPlugState(@PathVariable String name) {
    return plugsModel.getPlug(name);
  }

  @GetMapping("/api/plugs")
  public List<Plug> getAllPlugStates() {
    return plugsModel.getAllPlugs();
  }

  @GetMapping(value = "/api/plugs/{name:.+}", params = "action")
  public void controlPlug(@PathVariable String name, @RequestParam String action) {
    plugsModel.performAction(name, action);
  }

  @GetMapping("/api/plugs/{name:.+}/history")
  public List<MqttController.TimeStampedReading> getHistory(
      @PathVariable String name,
      @RequestParam(defaultValue="1") long minutes) {
    return plugsModel.getHistory(name, minutes);
  }
}