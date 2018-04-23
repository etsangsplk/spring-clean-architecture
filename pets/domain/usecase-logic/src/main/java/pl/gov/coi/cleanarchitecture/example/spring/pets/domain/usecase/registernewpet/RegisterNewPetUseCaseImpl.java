package pl.gov.coi.cleanarchitecture.example.spring.pets.domain.usecase.registernewpet;

import lombok.RequiredArgsConstructor;
import pl.gov.coi.cleanarchitecture.example.spring.pets.domain.model.entity.Person;
import pl.gov.coi.cleanarchitecture.example.spring.pets.domain.model.entity.Pet;
import pl.gov.coi.cleanarchitecture.example.spring.pets.domain.model.gateway.PersonGateway;
import pl.gov.coi.cleanarchitecture.example.spring.pets.domain.model.gateway.PetsGateway;
import pl.gov.coi.cleanarchitecture.example.spring.pets.domain.usecase.registernewpet.RegisterNewPetRequestModel.Ownership;
import pl.gov.coi.cleanarchitecture.example.spring.pets.domain.usecase.registernewpet.RegisterNewPetResponseModel.Violation;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author <a href="mailto:krzysztof.suszynski@coi.gov.pl">Krzysztof Suszynski</a>
 * @since 19.12.16
 */
@RequiredArgsConstructor
class RegisterNewPetUseCaseImpl implements RegisterNewPetUseCase {
  private final PetsGateway petsGateway;
  private final PersonGateway personGateway;
  private final RegisterNewPetRequestToPetMapper mapper;
  private final Validator validator;

  @Override
  public void execute(RegisterNewPetRequest request,
                      RegisterNewPetResponse response) {
    Set<ConstraintViolation<RegisterNewPetRequest>> violations = validator
      .validate(request);
    if (violations.isEmpty()) {
      Pet pet = mapper.asPet(request, this::ownershipAsPerson);
      petsGateway.persistNew(pet);
    } else {
      response.setViolations(toResponseModel(violations));
    }
  }

  private Person ownershipAsPerson(Ownership ownership) {
    Optional<Person> personOptional = personGateway
      .findByNameAndSurname(ownership.getName(), ownership.getSurname());
    return personOptional.orElse(new Person(
      ownership.getName(), ownership.getSurname()
    ));
  }

  private static Violation toViolation(ConstraintViolation<RegisterNewPetRequest> violation) {
    return new Violation(
      violation.getPropertyPath(),
      violation.getMessage()
    );
  }

  private static Iterable<Violation> toResponseModel(
    Iterable<ConstraintViolation<RegisterNewPetRequest>> violations) {
    return StreamSupport.stream(violations.spliterator(), false)
      .map(RegisterNewPetUseCaseImpl::toViolation)
      .collect(Collectors.toSet());
  }
}
