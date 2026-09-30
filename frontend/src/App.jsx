import React,{useEffect,useState} from "react";

async function api(url,options={}){
  const r=await fetch(url,{headers:{"Content-Type":"application/json","X-User":"recepcion1","X-Role":"RECEPCION",...(options.headers||{})},...options});
  if(!r.ok){let m="Error";try{m=(await r.json()).error||m}catch{}throw new Error(m)}
  return r.json();
}

export default function App(){
  const [doctores,setDoctores]=useState([]),[pacientes,setPacientes]=useState([]),[citas,setCitas]=useState([]),[recordatorios,setRecordatorios]=useState([]),[audit,setAudit]=useState([]),[metricas,setMetricas]=useState({});
  const [horarios,setHorarios]=useState([]),[tiempo,setTiempo]=useState(null),[mensaje,setMensaje]=useState("");
  const d=new Date();d.setDate(d.getDate()+1);
  const [form,setForm]=useState({doctorId:"",pacienteId:"",fecha:d.toISOString().slice(0,10),inicio:"",motivoConsulta:"Consulta general"});

  const cargar=async()=>{const [ds,ps,cs,rs,as,ms]=await Promise.all([api("/api/doctores"),api("/api/pacientes"),api("/api/citas"),api("/api/recordatorios"),api("/api/auditoria"),api("/api/metricas")]);setDoctores(ds);setPacientes(ps);setCitas(cs);setRecordatorios(rs);setAudit(as);setMetricas(ms);setForm(f=>({...f,doctorId:f.doctorId||String(ds[0]?.id||""),pacienteId:f.pacienteId||String(ps[0]?.id||"")}));};
  useEffect(()=>{cargar()},[]);

  const buscar=async()=>{try{const x=await api(`/api/disponibilidad?doctorId=${form.doctorId}&fecha=${form.fecha}`);setHorarios(x.horarios);setTiempo(x.tiempoBusquedaMs);setMensaje(`Se encontraron ${x.horarios.length} horarios.`)}catch(e){setMensaje(e.message)}};
  const reservar=async()=>{if(!form.inicio){setMensaje("Selecciona un horario.");return}try{await api("/api/citas",{method:"POST",body:JSON.stringify({doctorId:Number(form.doctorId),pacienteId:Number(form.pacienteId),inicio:form.inicio,motivoConsulta:form.motivoConsulta})});setMensaje("Cita reservada.");setHorarios([]);setForm({...form,inicio:""});await cargar()}catch(e){setMensaje(e.message)}};
  const cancelar=async id=>{try{await api(`/api/citas/${id}/cancelar`,{method:"POST"});setMensaje("Cita cancelada.");await cargar()}catch(e){setMensaje(e.message)}};
  const estado=async(id,n)=>{try{await api(`/api/citas/${id}/estado?estado=${n}`,{method:"POST",headers:{"X-User":"doctor1","X-Role":"DOCTOR"}});setMensaje(`Estado: ${n}`);await cargar()}catch(e){setMensaje(e.message)}};
  const reintentar=async id=>{await api(`/api/recordatorios/${id}/reintentar`,{method:"POST"});setMensaje("Recordatorio marcado para reintento.");await cargar()};

  return <main>
    <header><h1>Citas médicas</h1><p>Disponibilidad · reservas · cancelaciones · recordatorios · auditoría</p></header>
    {mensaje&&<div className="notice">{mensaje}</div>}
    <section className="metrics">
      <article><span>Citas duplicadas</span><strong>{metricas.citasDuplicadas??0}</strong></article>
      <article><span>Ausencias</span><strong>{metricas.ausencias??0}</strong></article>
      <article><span>Recordatorios entregados</span><strong>{Number(metricas.recordatoriosEntregadosPorcentaje??0).toFixed(0)}%</strong></article>
      <article><span>Tiempo para encontrar turno</span><strong>{tiempo==null?"-":`${tiempo} ms`}</strong></article>
    </section>

    <section className="grid">
      <div className="panel"><h2>Buscar disponibilidad</h2>
        <label>Doctor<select value={form.doctorId} onChange={e=>setForm({...form,doctorId:e.target.value,inicio:""})}>{doctores.map(x=><option key={x.id} value={x.id}>{x.nombre} — {x.especialidad}</option>)}</select></label>
        <label>Fecha<input type="date" value={form.fecha} onChange={e=>setForm({...form,fecha:e.target.value,inicio:""})}/></label>
        <button onClick={buscar}>Buscar horarios</button>
        <div className="slots">{horarios.map(h=><button key={h} className={form.inicio===h?"slot selected":"slot"} onClick={()=>setForm({...form,inicio:h})}>{new Date(h).toLocaleTimeString([],{hour:"2-digit",minute:"2-digit"})}</button>)}</div>
      </div>
      <div className="panel"><h2>Reservar cita</h2>
        <label>Paciente<select value={form.pacienteId} onChange={e=>setForm({...form,pacienteId:e.target.value})}>{pacientes.map(x=><option key={x.id} value={x.id}>{x.nombre}</option>)}</select></label>
        <label>Motivo<input value={form.motivoConsulta} onChange={e=>setForm({...form,motivoConsulta:e.target.value})}/></label>
        <p><b>Horario:</b> {form.inicio?new Date(form.inicio).toLocaleString():"No seleccionado"}</p>
        <button onClick={reservar}>Reservar</button>
        <p className="hint">La agenda no guarda historia clínica ni diagnósticos; solo datos necesarios para coordinar la cita.</p>
      </div>
    </section>

    <section><h2>Citas</h2><div className="table"><table><thead><tr><th>Paciente</th><th>Doctor</th><th>Horario</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>
      {citas.map(c=><tr key={c.id}><td>{c.paciente.nombre}</td><td>{c.doctor.nombre}</td><td>{new Date(c.inicio).toLocaleString()}</td><td>{c.estado}</td><td><div className="actions">{c.estado==="RESERVADA"&&<><button className="secondary" onClick={()=>cancelar(c.id)}>Cancelar</button><button onClick={()=>estado(c.id,"COMPLETADA")}>Atendida</button><button className="warn" onClick={()=>estado(c.id,"AUSENTE")}>Ausente</button></>}</div></td></tr>)}
    </tbody></table></div></section>

    <section><h2>Recordatorios</h2><div className="table"><table><thead><tr><th>Cita</th><th>Destino</th><th>Estado</th><th>Intentos</th><th></th></tr></thead><tbody>
      {recordatorios.map(r=><tr key={r.id}><td>#{r.cita.id} — {r.cita.paciente.nombre}</td><td>{r.destino}</td><td>{r.estado}</td><td>{r.intentos}</td><td>{(r.estado==="FALLIDO"||r.estado==="REINTENTO")&&<button onClick={()=>reintentar(r.id)}>Reintentar</button>}</td></tr>)}
    </tbody></table></div></section>

    <section><h2>Auditoría</h2><div className="audit">{audit.slice(0,15).map(a=><div key={a.id}><strong>{a.accion}</strong><span>{a.usuario} ({a.rol})</span><span>{a.detalle}</span><small>{new Date(a.fecha).toLocaleString()}</small></div>)}</div></section>
  </main>
}
