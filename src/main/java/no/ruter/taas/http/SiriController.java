package no.ruter.taas.http;


import no.ruter.taas.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SiriController {

  @Autowired
  private SubscriptionService service;

  @Autowired
  private Environment env;

  @GetMapping(value = "/admin/status")
  public String getAdmin(Model model) {
    model.addAttribute("subscriptions", service.getAllSubscriptions());
    model.addAttribute("environment", env.getActiveProfiles()[0]);
    return "status";
  }
}
