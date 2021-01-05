package com.businessproject.demo.service;

import com.businessproject.demo.exeption.*;
import com.businessproject.demo.model.Customer;
import com.businessproject.demo.model.Product;
import com.businessproject.demo.model.PromoEvent;
import com.businessproject.demo.model.PromoInfo;
import com.businessproject.demo.repository.CustomerRepository;
import com.businessproject.demo.repository.ProductRepository;
import com.businessproject.demo.repository.PromoRepository;
import com.businessproject.demo.repository.SalesRepRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SalesRepService {
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private SalesRepRepository salesRepRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private PromoRepository promoRepository;

    public boolean existsSalesRepById(String id) {
        return salesRepRepository.existsById(id);
    }

    public void saveCustomer(Customer customer) throws PhoneNumberAlreadyExists, AuthorizationException {
        if (!salesRepRepository.existsById(customer.getManagedById())) {
            throw new AuthorizationException();
        }
        if (customerRepository.existsByPhoneNumber(customer.getPhoneNumber())) {
            throw new PhoneNumberAlreadyExists();
        }
        customerRepository.save(customer);
    }

    public List<Customer> getCustomers(String salesRepId) {
        return customerRepository.findAllByManagedById(salesRepId);
    }

    public Customer getCustomerById(String id) throws NonExistingCustomerException {
        Optional<Customer> customer = customerRepository.findById(id);
        if (customer.isPresent()) {
            return customer.get();
        } else {
            throw new NonExistingCustomerException();
        }
    }

    public void deleteRepresentativeById(String id) throws NonExistingCustomerException {
        if (customerRepository.existsById(id)) {
            customerRepository.deleteById(id);
        } else {
            throw new NonExistingCustomerException();
        }
    }

    public void updateCustomer(Customer customer) throws NonExistingCustomerException, AuthorizationException, PhoneNumberAlreadyExists {
        if (!customerRepository.existsById(customer.getId())) {
            throw new NonExistingCustomerException();
        }
        if (!salesRepRepository.existsById(customer.getManagedById())) {
            throw new AuthorizationException();
        }
        if (customerRepository.existsByPhoneNumber(customer.getPhoneNumber())) {
            Customer oldCustomer = customerRepository.findById(customer.getId()).get();
            if (!oldCustomer.getPhoneNumber().equals(customer.getPhoneNumber())) {
                throw new PhoneNumberAlreadyExists();
            }
        }
        customerRepository.save(customer);
    }

    private Date findCurrentDate() {
        return Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private String getStatus(Date startDate, Date endDate) {
        Date currentDate = findCurrentDate();
        if (currentDate.before(startDate)) {
            return "Upcoming";
        } else if (currentDate.after(endDate)) {
            return "Expired";
        } else {
            return "Active";
        }
    }

    public List<PromoInfo> getPromoInfo() {
        return promoRepository
                .findAll()
                .stream()
                .map(x -> new PromoInfo(x, productRepository
                        .findById(x.getProductId())
                        .orElse(null), getStatus(x.getStartDate(), x.getEndDate())
                ))
                .filter(y -> y.getPromoProduct() != null)
                .collect(Collectors.toList());

    }

    public List<Product> getNonPromoProducts() {
        return productRepository
                .findAll()
                .stream()
                .filter(product -> !promoRepository.existsByProductId(product.getId()))
                .collect(Collectors.toList());
    }

    public void savePromoEvent(PromoEvent promoEvent) throws AuthorizationException, NonExistingProductException, PromotionAlreadyActive, MismatchedDateException {
        if (!salesRepRepository.existsById(promoEvent.getManagedById())) {
            throw new AuthorizationException();
        }
        if (!productRepository.existsById(promoEvent.getProductId())) {
            throw new NonExistingProductException();
        }
        if (promoRepository.existsByProductId(promoEvent.getProductId())) {
            throw new PromotionAlreadyActive();
        }
        if (promoEvent.getEndDate().before(promoEvent.getStartDate())) {
            throw new MismatchedDateException("End date can't be before start date!");
        } else if (promoEvent.getEndDate().before(findCurrentDate())) {
            throw new MismatchedDateException("End date can't be before current date!");
        }
        promoRepository.save(promoEvent);
    }

    public Product getProductById(String targetId) throws NonExistingProductException {
        return productRepository.findById(targetId).orElseThrow(NonExistingProductException::new);
    }
}
