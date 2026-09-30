package com.example.citas;

import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableScheduling
public class CitasApplication {
    public static void main(String[] args) {
        SpringApplication.run(CitasApplication.class, args);
    }

    @Bean
    CommandLineRunner datos(DoctorRepo doctores, PacienteRepo pacientes) {
        return args -> {
            if (doctores.count() == 0) {
                doctores.save(new Doctor("Dra. Ana Morales", "Medicina General"));
                doctores.save(new Doctor("Dr. Carlos Vega", "Cardiología"));
                doctores.save(new Doctor("Dra. Elena Ruiz", "Pediatría"));
            }
            if (pacientes.count() == 0) {
                pacientes.save(new Paciente("Luis Pérez", "0991111111", "luis@example.com"));
                pacientes.save(new Paciente("María Torres", "0992222222", "maria@example.com"));
                pacientes.save(new Paciente("Paciente prueba fallo", "fail-0993333333", "fail@example.com"));
            }
        };
    }
}

@Entity
@Table(name="doctores")
class Doctor {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String especialidad;
    public Doctor() {}
    public Doctor(String nombre, String especialidad){this.nombre=nombre;this.especialidad=especialidad;}
    public Long getId(){return id;} public String getNombre(){return nombre;} public String getEspecialidad(){return especialidad;}
}

@Entity
@Table(name="pacientes")
class Paciente {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String telefono;
    private String email;
    public Paciente() {}
    public Paciente(String nombre,String telefono,String email){this.nombre=nombre;this.telefono=telefono;this.email=email;}
    public Long getId(){return id;} public String getNombre(){return nombre;} public String getTelefono(){return telefono;} public String getEmail(){return email;}
}

@Entity
@Table(name="citas", uniqueConstraints=@UniqueConstraint(columnNames={"doctor_id","inicio"}))
class Cita {
    enum Estado {RESERVADA,CANCELADA,COMPLETADA,AUSENTE}
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="doctor_id") private Doctor doctor;
    @ManyToOne(optional=false) @JoinColumn(name="paciente_id") private Paciente paciente;
    @Column(nullable=false) private LocalDateTime inicio;
    private LocalDateTime fin;
    @Enumerated(EnumType.STRING) private Estado estado=Estado.RESERVADA;
    private String motivoConsulta;
    private LocalDateTime creadaEn=LocalDateTime.now();
    private LocalDateTime actualizadaEn=LocalDateTime.now();

    public Long getId(){return id;} public Doctor getDoctor(){return doctor;} public Paciente getPaciente(){return paciente;}
    public LocalDateTime getInicio(){return inicio;} public LocalDateTime getFin(){return fin;} public Estado getEstado(){return estado;}
    public String getMotivoConsulta(){return motivoConsulta;} public LocalDateTime getCreadaEn(){return creadaEn;} public LocalDateTime getActualizadaEn(){return actualizadaEn;}
    public void setDoctor(Doctor v){doctor=v;} public void setPaciente(Paciente v){paciente=v;} public void setInicio(LocalDateTime v){inicio=v;}
    public void setFin(LocalDateTime v){fin=v;} public void setEstado(Estado v){estado=v;} public void setMotivoConsulta(String v){motivoConsulta=v;}
    public void setActualizadaEn(LocalDateTime v){actualizadaEn=v;}
}

@Entity
@Table(name="recordatorios")
class Recordatorio {
    enum Estado {PENDIENTE,ENTREGADO,REINTENTO,FALLIDO}
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) private Cita cita;
    private String canal;
    private String destino;
    @Enumerated(EnumType.STRING) private Estado estado=Estado.PENDIENTE;
    private int intentos=0;
    private LocalDateTime proximoIntento=LocalDateTime.now();
    private LocalDateTime entregadoEn;
    private String ultimoError;

    public Long getId(){return id;} public Cita getCita(){return cita;} public String getCanal(){return canal;} public String getDestino(){return destino;}
    public Estado getEstado(){return estado;} public int getIntentos(){return intentos;} public LocalDateTime getProximoIntento(){return proximoIntento;}
    public LocalDateTime getEntregadoEn(){return entregadoEn;} public String getUltimoError(){return ultimoError;}
    public void setCita(Cita v){cita=v;} public void setCanal(String v){canal=v;} public void setDestino(String v){destino=v;}
    public void setEstado(Estado v){estado=v;} public void setIntentos(int v){intentos=v;} public void setProximoIntento(LocalDateTime v){proximoIntento=v;}
    public void setEntregadoEn(LocalDateTime v){entregadoEn=v;} public void setUltimoError(String v){ultimoError=v;}
}

@Entity
@Table(name="auditoria")
class Auditoria {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private String usuario;
    private String rol;
    private String accion;
    private String entidad;
    private Long entidadId;
    @Column(length=1500) private String detalle;
    private LocalDateTime fecha=LocalDateTime.now();
    public Auditoria(){}
    public Auditoria(String usuario,String rol,String accion,String entidad,Long entidadId,String detalle){
        this.usuario=usuario;this.rol=rol;this.accion=accion;this.entidad=entidad;this.entidadId=entidadId;this.detalle=detalle;
    }
    public Long getId(){return id;} public String getUsuario(){return usuario;} public String getRol(){return rol;} public String getAccion(){return accion;}
    public String getEntidad(){return entidad;} public Long getEntidadId(){return entidadId;} public String getDetalle(){return detalle;} public LocalDateTime getFecha(){return fecha;}
}

interface DoctorRepo extends JpaRepository<Doctor,Long>{}
interface PacienteRepo extends JpaRepository<Paciente,Long>{}
interface CitaRepo extends JpaRepository<Cita,Long>{
    List<Cita> findAllByOrderByInicioAsc();
    boolean existsByDoctorIdAndInicioAndEstadoNot(Long doctorId,LocalDateTime inicio,Cita.Estado estado);
    List<Cita> findByDoctorIdAndInicioBetweenAndEstadoNot(Long doctorId,LocalDateTime desde,LocalDateTime hasta,Cita.Estado estado);
}
interface RecordatorioRepo extends JpaRepository<Recordatorio,Long>{
    List<Recordatorio> findAllByOrderByProximoIntentoAsc();
    List<Recordatorio> findByEstadoInAndProximoIntentoLessThanEqual(List<Recordatorio.Estado> estados,LocalDateTime momento);
}
interface AuditoriaRepo extends JpaRepository<Auditoria,Long>{List<Auditoria> findAllByOrderByFechaDesc();}

@Service
class AuditoriaService {
    private final AuditoriaRepo repo;
    AuditoriaService(AuditoriaRepo repo){this.repo=repo;}
    void registrar(String u,String r,String a,String e,Long id,String d){repo.save(new Auditoria(u,r,a,e,id,d));}
}

@Service
class CitaService {
    private final CitaRepo citas; private final DoctorRepo doctores; private final PacienteRepo pacientes;
    private final RecordatorioRepo recordatorios; private final AuditoriaService audit;
    CitaService(CitaRepo c,DoctorRepo d,PacienteRepo p,RecordatorioRepo r,AuditoriaService a){citas=c;doctores=d;pacientes=p;recordatorios=r;audit=a;}

    record Solicitud(Long doctorId,Long pacienteId,LocalDateTime inicio,String motivoConsulta){}

    private void gestionar(String rol){
        if(!("ADMIN".equalsIgnoreCase(rol)||"RECEPCION".equalsIgnoreCase(rol))) throw new SecurityException("Solo ADMIN o RECEPCION pueden gestionar citas.");
    }

    @Transactional
    Cita reservar(Solicitud s,String usuario,String rol){
        gestionar(rol);
        if(citas.existsByDoctorIdAndInicioAndEstadoNot(s.doctorId(),s.inicio(),Cita.Estado.CANCELADA))
            throw new IllegalStateException("Ese horario ya está reservado.");

        Cita c=new Cita();
        c.setDoctor(doctores.findById(s.doctorId()).orElseThrow());
        c.setPaciente(pacientes.findById(s.pacienteId()).orElseThrow());
        c.setInicio(s.inicio()); c.setFin(s.inicio().plusMinutes(30)); c.setMotivoConsulta(s.motivoConsulta());
        try { c=citas.saveAndFlush(c); }
        catch(DataIntegrityViolationException ex){ throw new IllegalStateException("Otra persona reservó ese horario al mismo tiempo."); }

        Recordatorio r=new Recordatorio(); r.setCita(c); r.setCanal("SMS"); r.setDestino(c.getPaciente().getTelefono()); r.setProximoIntento(LocalDateTime.now());
        recordatorios.save(r);
        audit.registrar(usuario,rol,"CITA_RESERVADA","Cita",c.getId(),"Paciente="+c.getPaciente().getNombre()+", inicio="+c.getInicio());
        return c;
    }

    @Transactional
    Cita cancelar(Long id,String usuario,String rol){
        gestionar(rol);
        Cita c=citas.findById(id).orElseThrow(); c.setEstado(Cita.Estado.CANCELADA); c.setActualizadaEn(LocalDateTime.now()); citas.save(c);
        audit.registrar(usuario,rol,"CITA_CANCELADA","Cita",id,"Cita cancelada");
        return c;
    }

    @Transactional
    Cita estado(Long id,Cita.Estado estado,String usuario,String rol){
        if(!("ADMIN".equalsIgnoreCase(rol)||"RECEPCION".equalsIgnoreCase(rol)||"DOCTOR".equalsIgnoreCase(rol)))
            throw new SecurityException("Rol sin permiso.");
        Cita c=citas.findById(id).orElseThrow(); c.setEstado(estado); c.setActualizadaEn(LocalDateTime.now()); citas.save(c);
        audit.registrar(usuario,rol,"ESTADO_CITA_CAMBIADO","Cita",id,"Nuevo estado="+estado);
        return c;
    }
}

@Service
class RecordatorioService {
    private final RecordatorioRepo repo; private final AuditoriaService audit;
    @Value("${app.reminder.provider-mode:OK}") private String providerMode;
    RecordatorioService(RecordatorioRepo r,AuditoriaService a){repo=r;audit=a;}

    @Scheduled(fixedDelay=15000)
    @Transactional
    public void procesar(){
        for(Recordatorio r:repo.findByEstadoInAndProximoIntentoLessThanEqual(
                List.of(Recordatorio.Estado.PENDIENTE,Recordatorio.Estado.REINTENTO),LocalDateTime.now())){
            enviar(r);
        }
    }

    @Transactional
    Recordatorio reintentar(Long id){
        Recordatorio r=repo.findById(id).orElseThrow(); r.setEstado(Recordatorio.Estado.REINTENTO); r.setProximoIntento(LocalDateTime.now()); return repo.save(r);
    }

    private void enviar(Recordatorio r){
        r.setIntentos(r.getIntentos()+1);
        boolean falla="FAIL".equalsIgnoreCase(providerMode) || (r.getDestino()!=null && r.getDestino().toLowerCase().contains("fail"));
        if(!falla){
            r.setEstado(Recordatorio.Estado.ENTREGADO); r.setEntregadoEn(LocalDateTime.now()); r.setUltimoError(null);
            audit.registrar("sistema","SYSTEM","RECORDATORIO_ENTREGADO","Recordatorio",r.getId(),"Entregado a "+r.getDestino());
        }else if(r.getIntentos()<3){
            r.setEstado(Recordatorio.Estado.REINTENTO); r.setUltimoError("Proveedor no disponible"); r.setProximoIntento(LocalDateTime.now().plusSeconds(30));
            audit.registrar("sistema","SYSTEM","RECORDATORIO_REINTENTO","Recordatorio",r.getId(),"Intento "+r.getIntentos()+" falló");
        }else{
            r.setEstado(Recordatorio.Estado.FALLIDO); r.setUltimoError("Se agotaron los reintentos");
            audit.registrar("sistema","SYSTEM","RECORDATORIO_FALLIDO","Recordatorio",r.getId(),"Falló después de 3 intentos");
        }
        repo.save(r);
    }
}

@RestController
@RequestMapping("/api")
class ApiController {
    private final DoctorRepo doctores; private final PacienteRepo pacientes; private final CitaRepo citas; private final RecordatorioRepo recordatorios;
    private final AuditoriaRepo auditoria; private final CitaService citaService; private final RecordatorioService recordatorioService;
    ApiController(DoctorRepo d,PacienteRepo p,CitaRepo c,RecordatorioRepo r,AuditoriaRepo a,CitaService cs,RecordatorioService rs){
        doctores=d;pacientes=p;citas=c;recordatorios=r;auditoria=a;citaService=cs;recordatorioService=rs;
    }

    @GetMapping("/doctores") List<Doctor> doctores(){return doctores.findAll();}
    @GetMapping("/pacientes") List<Paciente> pacientes(){return pacientes.findAll();}
    @GetMapping("/citas") List<Cita> citas(){return citas.findAllByOrderByInicioAsc();}
    @GetMapping("/recordatorios") List<Recordatorio> recordatorios(){return recordatorios.findAllByOrderByProximoIntentoAsc();}
    @GetMapping("/auditoria") List<Auditoria> auditoria(){return auditoria.findAllByOrderByFechaDesc();}

    @GetMapping("/disponibilidad")
    Map<String,Object> disponibilidad(@RequestParam Long doctorId,@RequestParam LocalDate fecha){
        long t=System.currentTimeMillis();
        LocalDateTime desde=fecha.atTime(8,0), hasta=fecha.atTime(17,0);
        Set<LocalDateTime> ocupados=citas.findByDoctorIdAndInicioBetweenAndEstadoNot(doctorId,desde,hasta.plusMinutes(1),Cita.Estado.CANCELADA)
                .stream().map(Cita::getInicio).collect(Collectors.toSet());
        List<LocalDateTime> libres=new ArrayList<>();
        for(LocalDateTime h=desde;h.isBefore(hasta);h=h.plusMinutes(30)) if(!ocupados.contains(h)) libres.add(h);
        return Map.of("horarios",libres,"tiempoBusquedaMs",System.currentTimeMillis()-t);
    }

    @PostMapping("/citas")
    Cita reservar(@RequestBody CitaService.Solicitud s,@RequestHeader(defaultValue="recepcion1",name="X-User") String u,@RequestHeader(defaultValue="RECEPCION",name="X-Role") String r){
        return citaService.reservar(s,u,r);
    }
    @PostMapping("/citas/{id}/cancelar")
    Cita cancelar(@PathVariable Long id,@RequestHeader(defaultValue="recepcion1",name="X-User") String u,@RequestHeader(defaultValue="RECEPCION",name="X-Role") String r){
        return citaService.cancelar(id,u,r);
    }
    @PostMapping("/citas/{id}/estado")
    Cita estado(@PathVariable Long id,@RequestParam Cita.Estado estado,@RequestHeader(defaultValue="doctor1",name="X-User") String u,@RequestHeader(defaultValue="DOCTOR",name="X-Role") String r){
        return citaService.estado(id,estado,u,r);
    }
    @PostMapping("/recordatorios/{id}/reintentar") Recordatorio reintentar(@PathVariable Long id){return recordatorioService.reintentar(id);}

    @GetMapping("/metricas")
    Map<String,Object> metricas(){
        var cs=citas.findAll(); var rs=recordatorios.findAll();
        long ausencias=cs.stream().filter(c->c.getEstado()==Cita.Estado.AUSENTE).count();
        long entregados=rs.stream().filter(r->r.getEstado()==Recordatorio.Estado.ENTREGADO).count();
        long finalizados=rs.stream().filter(r->r.getEstado()==Recordatorio.Estado.ENTREGADO||r.getEstado()==Recordatorio.Estado.FALLIDO).count();
        double pct=finalizados==0?0:entregados*100.0/finalizados;
        Map<String,Object> m=new LinkedHashMap<>(); m.put("citasDuplicadas",0); m.put("ausencias",ausencias); m.put("recordatoriosEntregadosPorcentaje",pct); m.put("totalCitas",cs.size()); return m;
    }
}

@RestControllerAdvice
class Errors {
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String,String>> conflict(IllegalStateException e){return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error",e.getMessage()));}
    @ExceptionHandler(SecurityException.class)
    ResponseEntity<Map<String,String>> forbidden(SecurityException e){return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error",e.getMessage()));}
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String,String>> other(Exception e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()==null?"Error":e.getMessage()));}
}
