import React from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import Navbar from '../components/Navbar';
import FloatingDock from '../components/FloatingDock';
import UserCard from '../components/UserCard';
import CreatePostModal from '../components/CreatePostModal';
import { usePrompt } from '../context/PromptContext';
import { useNetwork } from '../context/NetworkContext';

const Network = ({ session, onLogout }) => {
  const navigate = useNavigate();
  const { showPrompt } = usePrompt();
  
  const {
    searchQuery,
    setSearchQuery,
    isModalOpen,
    setIsModalOpen,
    pendingInvites,
    loading,
    handleAcceptInvite,
    handleRejectInvite,
    handleStatusChange,
    filteredUsers,
    recommendations
  } = useNetwork();

  return (
    <div className="h-screen md:min-h-screen md:h-auto bg-rgukt-slate flex flex-col font-sans overflow-hidden md:overflow-visible">
      <Navbar 
        isLanding={false} 
        searchQuery={searchQuery} 
        setSearchQuery={setSearchQuery} 
        session={session}
        onLogout={onLogout}
        onPlusClick={() => setIsModalOpen(true)}
      />

      <main className="flex-1 overflow-y-auto md:overflow-visible min-h-0 md:min-h-auto w-full max-w-6xl mx-auto px-4 pt-8 pb-32 md:pb-60">
        <div className="flex justify-between items-end mb-8 px-2">
          <div>
            <h2 className="text-2xl font-bold text-charcoal tracking-tight">Alumni Directory</h2>
            <p className="text-slate-500 text-sm mt-1">
              Connect with verified seniors and peers from RGUKT
            </p>
          </div>
          
          <span className="text-xs font-bold text-slate-400 bg-white px-3 py-1 rounded-full border border-slate-100 shadow-sm">
            {filteredUsers.length} People
          </span>
        </div>
 
        {pendingInvites.length > 0 && (
          <div className="bg-white p-6 rounded-[32px] border border-slate-100 shadow-sm mb-8">
            <h3 className="font-bold text-charcoal mb-4 flex items-center gap-2 text-sm uppercase tracking-wider">
              <span className="w-2.5 h-2.5 rounded-full bg-rgukt-maroon animate-pulse"></span>
              Pending Invites ({pendingInvites.length})
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
              {pendingInvites.map(invite => (
                <div key={invite.id} className="p-4 bg-slate-50 border border-slate-100 rounded-2xl flex items-center justify-between shadow-xs">
                  <div className="flex items-center gap-3 min-w-0">
                    <div className="w-10 h-10 rounded-full bg-rgukt-slate text-rgukt-maroon font-bold flex items-center justify-center text-sm shadow-sm border border-slate-200">
                      {invite.sender.name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2)}
                    </div>
                    <div className="min-w-0">
                      <h5 className="font-bold text-charcoal text-xs truncate">{invite.sender.name}</h5>
                      <p className="text-[10px] text-slate-400">ID: {invite.sender.idNumber}</p>
                    </div>
                  </div>
                  <div className="flex gap-2 shrink-0">
                    <button 
                      onClick={() => handleAcceptInvite(invite.id, showPrompt)} 
                      className="bg-rgukt-maroon text-white px-3 py-1.5 rounded-lg text-xs font-bold hover:scale-[1.02] transition-transform cursor-pointer"
                    >
                      Accept
                    </button>
                    <button 
                      onClick={() => handleRejectInvite(invite.id, showPrompt)} 
                      className="border border-slate-200 text-slate-600 px-3 py-1.5 rounded-lg text-xs font-bold hover:bg-white transition-all cursor-pointer"
                    >
                      Ignore
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Recommended Connections (People You May Know) */}
        {recommendations.length > 0 && (
          <div className="bg-white p-6 rounded-[32px] border border-slate-100 shadow-sm mb-8">
            <h3 className="font-bold text-charcoal mb-1.5 flex items-center gap-2 text-sm uppercase tracking-wider">
              <span className="w-2.5 h-2.5 rounded-full bg-rgukt-gold animate-pulse"></span>
              People You May Know
            </h3>
            <p className="text-xs text-slate-400 mb-4 font-semibold">Recommended via professional graph (mutual connections)</p>
            
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
              {recommendations.map(rec => (
                <div key={rec.id} className="p-4 bg-slate-50/65 border border-slate-100 rounded-2xl flex flex-col justify-between shadow-xs relative overflow-hidden group">
                  <div className="flex gap-3 min-w-0">
                    <div className="w-12 h-12 rounded-full bg-rgukt-maroon/10 border border-rgukt-maroon/20 text-rgukt-maroon font-black flex items-center justify-center text-sm shadow-sm shrink-0 overflow-hidden">
                      {rec.profilePhoto ? (
                        <img src={rec.profilePhoto} alt={rec.name} className="w-full h-full object-cover" />
                      ) : (
                        rec.name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2)
                      )}
                    </div>
                    
                    <div className="min-w-0 flex-1">
                      <h5 className="font-bold text-charcoal text-sm truncate group-hover:text-rgukt-maroon transition-colors">{rec.name}</h5>
                      <p className="text-[10px] text-slate-400 font-medium">{rec.role} • {rec.idNumber}</p>
                      
                      <div className="mt-2 flex items-center gap-1">
                        <span className="text-[10px] bg-rgukt-gold/15 text-amber-700 font-black px-1.5 py-0.5 rounded-full">
                          {rec.mutualCount} Mutual Connection{rec.mutualCount > 1 ? 's' : ''}
                        </span>
                      </div>
                      
                      {rec.mutualFriends && rec.mutualFriends.length > 0 && (
                        <p className="text-[9px] text-slate-400 mt-1 truncate">
                          Mutual: <span className="font-bold text-slate-500">{rec.mutualFriends.join(', ')}</span>
                        </p>
                      )}
                    </div>
                  </div>
                  
                  <div className="mt-4 flex gap-2 w-full">
                    <button 
                      onClick={() => navigate(`/profile?userId=${rec.id}`)}
                      className="flex-1 bg-white border border-slate-200 text-slate-700 py-1.5 rounded-xl text-xs font-bold hover:bg-slate-50 active:scale-[0.98] transition-all cursor-pointer text-center"
                    >
                      Profile
                    </button>
                    <button 
                      onClick={async () => {
                        try {
                          await axios.post(`/api/connections/request/${rec.id}`, {}, {
                            headers: { Authorization: `Bearer ${session.token}` }
                          });
                          handleStatusChange();
                        } catch (err) {
                          console.error(err);
                        }
                      }}
                      className="flex-1 bg-rgukt-maroon text-white py-1.5 rounded-xl text-xs font-bold hover:shadow-md hover:scale-[1.02] active:scale-[0.98] transition-all cursor-pointer text-center"
                    >
                      Connect
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {loading ? (
          <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {[...Array(6)].map((_, i) => (
              <div key={i} className="bg-white rounded-3xl border border-slate-100 p-5 flex flex-col items-center text-center shadow-sm animate-pulse">
                <div className="w-20 h-20 rounded-full bg-slate-100 mb-4" />
                <div className="h-4 bg-slate-150 rounded-full w-2/3 mb-2" />
                <div className="h-3 bg-slate-100 rounded-full w-1/2 mb-3" />
                <div className="h-3 bg-slate-100 rounded-full w-3/4 mb-5" />
                <div className="h-9 bg-slate-100 rounded-xl w-full" />
              </div>
            ))}
          </section>
        ) : (
          <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredUsers.map(user => (
              <UserCard 
                key={user.id} 
                user={user} 
                session={session} 
                onStatusChange={handleStatusChange}
              />
            ))}
          </section>
        )}
        {filteredUsers.length === 0 && (
          <div className="bg-white p-20 rounded-[32px] border border-slate-100 text-center shadow-sm">
             <div className="text-4xl mb-4 opacity-20">👥</div>
             <p className="text-slate-400 italic">No alumni found matching "{searchQuery}"</p>
          </div>
        )}
      </main>

      <CreatePostModal 
        isOpen={isModalOpen} 
        onClose={() => setIsModalOpen(false)} 
        onSubmit={(newPost) => {
            setIsModalOpen(false);
            navigate('/home');
        }}
        session={session}
      />

      <FloatingDock onPlusClick={() => setIsModalOpen(true)} />
    </div>
  );
};

export default Network;