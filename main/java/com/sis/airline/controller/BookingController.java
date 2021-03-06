package com.sis.airline.controller;


import com.sis.airline.exceptions.AgeLimitException;
import com.sis.airline.model.Booking;
import com.sis.airline.model.Flight;
import com.sis.airline.model.Passenger;
import com.sis.airline.repository.BookingRepository;
import com.sis.airline.repository.FlightRepository;
import com.sis.airline.repository.PassengerRepository;
import com.sis.airline.service.PassengerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.ParseException;
import java.util.Date;
import java.util.Random;

@Controller
public class BookingController {

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PassengerRepository passengerRepository;

   @Autowired
    private PassengerService passengerService;

    @RequestMapping(value = "/bookings/list", method = RequestMethod.GET)
    public String bookings(Model model){
        model.addAttribute("bookings",bookingRepository.findAll());
        //*model.addAttribute("message","Thank You For Flying With Us");*//*
        return "booking/list";
    }
    @RequestMapping(value = "/bookings/bookFlight/{id}", method = RequestMethod.GET)
    public String create(@PathVariable("id") int id, Model model) {

        model.addAttribute("flight", flightRepository.findById(id).get());
        return "booking/bookFlight";
    }

    public String generateUniqueId() {
        Random rand = new Random(); //instance of random class
        //generate random values from 0-100000
        int num = rand.nextInt(100000);
        String uniqueId = ("00" + Integer.toString(num));
        return uniqueId;
    }

    @RequestMapping(value = "/bookings/process", method = RequestMethod.POST)
    public String processBooking(Model model, @RequestParam int id, @RequestParam String lastName, @RequestParam String firstName, @RequestParam String email, @RequestParam String address, @RequestParam String phone, @RequestParam int age) {

        Flight flight = flightRepository.findById(id).get();
        int availableSeat = flight.getAvailableSeat();

        Passenger passenger = new Passenger(lastName, firstName, address, email, phone, age );
        try {
            boolean succeed = passengerService.addPassenger(passenger);
            if (!succeed) {
              //  model.addAttribute("failed", "Passenger age must be above 18 !!!");
                return "booking/failed";
            }
        }catch (AgeLimitException agEx){
            model.addAttribute("failed", "Passenger age must be above 18 !!!");
            return "booking/failed";
        }

        long millis = System.currentTimeMillis();
        Date bookingDate = new Date(millis);

        Booking b = null; String bookingNumber = " ";
        do{
            bookingNumber = generateUniqueId();
            b = bookingRepository.findBookingByBookingNumber(bookingNumber);
        }while (b != null);

        Booking booking = new Booking(bookingNumber, flight, passenger, bookingDate, availableSeat);
        bookingRepository.save(booking);

        flight.setAvailableSeat(availableSeat - 1);
        flightRepository.save(flight);

        model.addAttribute("flight", flight);
        model.addAttribute("booking", booking);
        model.addAttribute("passenger", passenger);
        model.addAttribute("success", "Thank you for flying with us, bellow is the details of your booking. A copy of this has been sent to your email...");

        return "booking/success";
    }

    @RequestMapping(value = "/bookings/edit/{id}", method = RequestMethod.GET)
    public String showUpdateForm(@PathVariable("id") int id, Model model) {

        model.addAttribute("booking", bookingRepository.findById(id).get());
        return "booking/edit";
    }

    @RequestMapping(value = "/bookings/update", method = RequestMethod.POST)
    public String updateBooking(Model model, @RequestParam int id, @RequestParam String bookingNumber, @RequestParam int seatNo) throws ParseException {

        Booking booking = bookingRepository.findById(id).get();

        booking.setSeatNo(seatNo);

        bookingRepository.save(booking);

        return "redirect:/bookings/list";
    }

    @RequestMapping(value = "/bookings/delete/{id}", method = RequestMethod.GET)
    public String remove(@PathVariable("id") int id, Model model) {

        Booking booking = bookingRepository.findById(id).get();

        bookingRepository.delete(booking);
        return "redirect:/bookings/list";
    }
}
