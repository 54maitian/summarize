package model;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/** @author T00032266 @DateTime 2021/5/17 */
@Component
public class Profession {

	public Person getPerson() {
		return new Person();
	}

	public static Person getStaticPerson() {
		return new Person();
	}
}
