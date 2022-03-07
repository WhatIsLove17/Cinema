package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}

class Seat {
    private int row;
    private int column;
    private int price;

    public Seat(int row, int column, int price) {
        this.row = row;
        this.column = column;
        this.price = price;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public static Comparator<Seat> getComp()
    {
        Comparator comp = new Comparator<Seat>(){
            @Override
            public int compare(Seat s1, Seat s2)
            {
                if (s1.getRow() > s2.getRow())
                    return 1;
                else if(s1.getRow() < s2.getRow())
                    return -1;
                else{
                    if (s1.getColumn() > s2.getColumn())
                        return 1;
                    else if (s1.getColumn() < s2.getColumn())
                        return -1;
                    else return 0;
                }
            }
        };
        return comp;
    }

    @Override
    public boolean equals(Object obj){
        if (obj instanceof Seat){
            if (this.row == ((Seat) obj).row && this.column == ((Seat) obj).column && this.price == ((Seat) obj).price)
                return true;
        }
        return false;
    }


}

class Seats {
    public int total_rows;
    public int total_columns;
    public CopyOnWriteArrayList<Seat> available_seats;

    {
        total_rows = 9;
        total_columns = 9;
        available_seats = new CopyOnWriteArrayList<>();
    }
}

class RequestSeat {
    private int row;
    private int column;

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }
}

class Ticket {
    public String token;
    public Seat ticket;

    Ticket(String token, Seat ticket) {
        this.ticket = ticket;
        this.token = token;
    }
}

class Stats{
    public int current_income = 0;
    public int number_of_available_seats = 81;
    public int number_of_purchased_tickets = 0;
}

@RestController
class CinemaController {
    private Seats availableSeats;
    private ConcurrentHashMap<String, Seat> ticketsSold;
    private CopyOnWriteArrayList<Seat> allSeats;
    private CopyOnWriteArrayList<Boolean> bookedPlaces;
    private Stats stats;

    {
        stats = new Stats();
        ticketsSold = new ConcurrentHashMap<>();
        bookedPlaces = new CopyOnWriteArrayList<>();
        allSeats = new CopyOnWriteArrayList<>();
        availableSeats = new Seats();
        for (int row = 1; row <= 9; row++) {
            for (int column = 1; column <= 9; column++) {
                bookedPlaces.add(false);
                if (row <= 4) {
                    availableSeats.available_seats.add(new Seat(row, column, 10));
                    allSeats.add(new Seat(row, column, 10));
                } else {
                    availableSeats.available_seats.add(new Seat(row, column, 8));
                    allSeats.add(new Seat(row, column, 8));
                }
            }
        }
    }

    @PostMapping("/stats")
    public ResponseEntity stats(@Nullable @RequestParam String password){
        if (password == null)
            return new ResponseEntity(Map.of("error", "The password is wrong!"), HttpStatus.UNAUTHORIZED);
        if (password.equals("super_secret")){
            return new ResponseEntity(stats, HttpStatus.OK);
        }
        else{
            return new ResponseEntity(Map.of("error", "The password is wrong!"), HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/purchase")
    public ResponseEntity purchase(@RequestBody RequestSeat seat) {
        if (seat.getColumn() > 9 || seat.getColumn() < 1 || seat.getRow() > 9 || seat.getRow() < 1)
            return new ResponseEntity(Map.of("error", "The number of a row or a column is out of bounds!"), HttpStatus.BAD_REQUEST);

        int index = (seat.getRow() - 1) * 9 + seat.getColumn() - 1;
        if (bookedPlaces.get(index))
            return new ResponseEntity(Map.of("error", "The ticket has been already purchased!"), HttpStatus.BAD_REQUEST);

        Ticket ticket = new Ticket(UUID.randomUUID().toString(), allSeats.get(index));
        stats.current_income += ticket.ticket.getPrice();
        stats.number_of_available_seats -= 1;
        stats.number_of_purchased_tickets += 1;
        ticketsSold.put(ticket.token, ticket.ticket);
        bookedPlaces.set(index, true);
        availableSeats.available_seats.remove(ticket.ticket);
        return new ResponseEntity(ticket, HttpStatus.OK);
    }

    @PostMapping("/return")
    public ResponseEntity returnTicket(@RequestBody Map<String, String> token) {
        if (ticketsSold.containsKey(token.get("token"))) {
            int index = (ticketsSold.get(token.get("token")).getRow() - 1) * 9 + ticketsSold.get(token.get("token")).getColumn() - 1;
            stats.current_income -= allSeats.get(index).getPrice();
            stats.number_of_purchased_tickets -= 1;
            stats.number_of_available_seats += 1;
            ticketsSold.remove(token.get("token"));
            availableSeats.available_seats.add(allSeats.get(index));
            availableSeats.available_seats.sort(Seat.getComp());
            bookedPlaces.set(index, false);
            return new ResponseEntity(Map.of("returned_ticket", allSeats.get(index)), HttpStatus.OK);
        }
        else{
            return new ResponseEntity(Map.of("error", "Wrong token!"), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/seats")
    public Seats availableSeats() {
        return availableSeats;
    }

}


