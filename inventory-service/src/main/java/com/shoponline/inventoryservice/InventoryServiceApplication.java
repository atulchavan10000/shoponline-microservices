package com.shoponline.inventoryservice;

import com.shoponline.inventoryservice.model.Inventory;
import com.shoponline.inventoryservice.repository.InventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class InventoryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryServiceApplication.class, args);
	}

	// to create a data at startup
	@Bean
	public CommandLineRunner loadData(InventoryRepository inventoryRepository){
		return args -> {
			Inventory inventory =  new Inventory();
			inventory.setSkuCode("iPhone 13");
			inventory.setQuantity(100);

			Inventory inventory1 =  new Inventory();
			inventory1.setSkuCode("iPhone 13 red");
			inventory1.setQuantity(0);

			inventoryRepository.save(inventory);
			inventoryRepository.save(inventory1);
		};
	}
}
